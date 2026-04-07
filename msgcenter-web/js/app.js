/**
 * MsgCenter Web Console - 主逻辑
 */

// ============ 全局状态 ============
let allTemplates = [];           // 所有模板列表
let sendHistory = [];            // 发送历史
let editingTemplateId = null;     // 当前编辑的模板 ID

// ============ 初始化 ============
document.addEventListener('DOMContentLoaded', () => {
    initNav();
    initForms();
    checkSystemStatus();
    loadTemplateList();
});

// ============ 导航切换 ============
function initNav() {
    document.querySelectorAll('.nav-item').forEach(item => {
        item.addEventListener('click', () => {
            const tab = item.dataset.tab;
            switchTab(tab);
        });
    });
}

function switchTab(tabName) {
    // 更新导航高亮
    document.querySelectorAll('.nav-item').forEach(item => {
        item.classList.toggle('active', item.dataset.tab === tabName);
    });
    // 切换面板
    document.querySelectorAll('.tab-panel').forEach(panel => {
        panel.classList.toggle('active', panel.id === `panel-${tabName}`);
    });
    // 面板激活时的额外操作
    if (tabName === 'template') {
        loadTemplateList();
    } else if (tabName === 'send') {
        renderSendTemplateList();
    } else if (tabName === 'dashboard') {
        refreshDashboard();
    }
}

// ============ 表单初始化 ============
function initForms() {
    // 模板表单
    document.getElementById('template-form').addEventListener('submit', handleTemplateSubmit);

    // 发送表单
    document.getElementById('send-form').addEventListener('submit', handleSendSubmit);

    // 记录查询表单
    document.getElementById('record-form').addEventListener('submit', handleRecordQuery);

    // 定时时间戳联动
    document.getElementById('send-timestamp').addEventListener('input', () => {
        const tsInput = document.getElementById('send-timestamp');
        const msInput = document.getElementById('send-timestamp-ms');
        if (tsInput.value) {
            msInput.value = new Date(tsInput.value).getTime().toString();
        }
    });
    document.getElementById('send-timestamp-ms').addEventListener('input', () => {
        const msInput = document.getElementById('send-timestamp-ms');
        const tsInput = document.getElementById('send-timestamp');
        if (msInput.value) {
            tsInput.value = formatDateTimeLocal(parseInt(msInput.value));
        }
    });
}

// ============ Toast 通知 ============
function showToast(message, type = 'info', duration = 3000) {
    const container = document.getElementById('toast-container');
    const icons = { success: '✓', error: '✗', warning: '⚠', info: 'ℹ' };
    const toast = document.createElement('div');
    toast.className = `toast ${type}`;
    toast.innerHTML = `
        <span class="toast-icon">${icons[type]}</span>
        <span class="toast-msg">${message}</span>
        <span class="toast-close" onclick="this.parentElement.remove()">×</span>
    `;
    container.appendChild(toast);
    setTimeout(() => toast.remove(), duration);
}

// ============ 系统状态 ============
async function checkSystemStatus() {
    const dot = document.getElementById('systemStatusDot');
    const text = document.getElementById('systemStatusText');
    const ok = await apiHealthCheck();
    if (ok) {
        dot.className = 'status-dot online';
        text.textContent = '系统正常';
    } else {
        dot.className = 'status-dot offline';
        text.textContent = '服务不可达';
    }
}

async function refreshDashboard() {
    await checkSystemStatus();

    // 从配置推断信息
    document.getElementById('dash-mq-mode').textContent =
        'Kafka 模式'; // 可通过后端接口暴露实际配置
    document.getElementById('dash-kafka').textContent = '127.0.0.1:9092';
    document.getElementById('dash-mysql').textContent = '127.0.0.1:3306/qin';
    document.getElementById('dash-redis').textContent = 'localhost:6379';

    // 熔断配置（硬编码匹配 application.yml）
    document.getElementById('cfg-threshold').textContent = '5 次连续失败';
    document.getElementById('cfg-open').textContent = '300 秒';
    document.getElementById('cfg-fallback').textContent = 'Email→飞书 | SMS→邮件 | Lark→邮件';

    // 熔断状态（实际需要后端接口，这里通过尝试发消息测试）
    await refreshCircuitStatus();
}

async function refreshCircuitStatus() {
    const channels = [1, 2, 3];
    for (const ch of channels) {
        const item = document.querySelector(`.circuit-item[data-channel="${ch}"]`);
        if (!item) continue;
        const badge = item.querySelector('.circuit-badge');
        // 尝试发一条测试消息，看是否会降级
        // 简化处理：前端只显示"正常"，实际由后端控制
        badge.className = 'circuit-badge normal';
        badge.textContent = '正常';
    }
}

// ============ 模板管理 ============
async function loadTemplateList() {
    const tbody = document.getElementById('template-tbody');
    tbody.innerHTML = '<tr><td colspan="6" class="empty-cell"><div class="empty-state"><span>⏳</span><p>加载中...</p></div></td></tr>';

    try {
        // 如果没有后端接口，先用模拟数据展示 UI
        allTemplates = await loadTemplatesFromBackend();
        renderTemplateTable(allTemplates);
    } catch (e) {
        // 后端不可达时显示示例数据
        allTemplates = getMockTemplates();
        renderTemplateTable(allTemplates);
    }
}

async function loadTemplatesFromBackend() {
    return await apiFindAllTemplate();
}

function getMockTemplates() {
    return [
        { id: 1, templateName: '订单确认通知', channel: 1, status: 2, content: '您好 ${username}，您的订单 ${orderId} 已确认，金额 ${amount} 元。' },
        { id: 2, templateName: '注册验证码', channel: 1, status: 2, content: '您的验证码是 ${code}，5 分钟内有效，请勿泄露。' },
        { id: 3, templateName: '活动通知', channel: 3, status: 2, content: '${title}活动开始了，点击查看：${url}' },
    ];
}

function renderTemplateTable(templates) {
    const tbody = document.getElementById('template-tbody');
    if (!templates || templates.length === 0) {
        tbody.innerHTML = `
            <tr class="empty-row">
                <td colspan="6" class="empty-cell">
                    <div class="empty-state">
                        <span>📭</span>
                        <p>暂无模板</p>
                        <p class="empty-hint">点击上方「新建模板」创建一个</p>
                    </div>
                </td>
            </tr>`;
        return;
    }

    const html = templates.map(t => {
        const channelMap = { 1: ['📧 Email', 'channel-email'], 2: ['📱 SMS', 'channel-sms'], 3: ['🦜 Lark', 'channel-lark'] };
        const statusMap = { 1: ['⏳ 待审', 'badge-pending'], 2: ['✅ 正常', 'badge-normal'] };
        const [chLabel, chClass] = channelMap[t.channel] || ['未知', ''];
        const [stLabel, stClass] = statusMap[t.status] || ['未知', ''];
        const preview = t.content ? t.content.slice(0, 50) + (t.content.length > 50 ? '…' : '') : '—';

        return `
            <tr data-id="${t.templateId}">
                <td><code>${t.templateId.slice(0,8)}…</code></td>
                <td><strong class="clickable-name" onclick="selectTemplateForSend('${t.templateId}')" title="点击使用此模板发送">${t.name}</strong></td>
                <td><span class="channel-tag ${chClass}">${chLabel}</span></td>
                <td><span class="badge ${stClass}">${stLabel}</span></td>
                <td><span class="content-preview" title="${escHtml(t.content || '')}">${escHtml(preview)}</span></td>
                <td>
                    <div class="action-btns">
                        <button class="action-btn" onclick="editTemplate('${t.templateId}')">✏️ 编辑</button>
                        <button class="action-btn danger" onclick="deleteTemplate('${t.templateId}')">🗑️ 删除</button>
                    </div>
                </td>
            </tr>`;
    }).join('');
    tbody.innerHTML = html;
}

function filterTemplates() {
    const keyword = document.getElementById('template-search').value.toLowerCase();
    if (!keyword) {
        renderTemplateTable(allTemplates);
        return;
    }
    const filtered = allTemplates.filter(t =>
        (t.name && t.name.toLowerCase().includes(keyword)) ||
        (t.content && t.content.toLowerCase().includes(keyword)) ||
        (t.templateId && t.templateId.includes(keyword))
    );
    renderTemplateTable(filtered);
}

// ============ 模板弹窗 ============
function showTemplateModal(templateId = null) {
    const modal = document.getElementById('template-modal');
    const form = document.getElementById('template-form');
    const title = document.getElementById('template-modal-title');

    form.reset();
    editingTemplateId = templateId;

    if (templateId) {
        title.textContent = '编辑模板';
        const t = allTemplates.find(x => x.templateId === templateId);
        if (t) {
            document.getElementById('tmpl-id').value = t.templateId;
            document.getElementById('tmpl-name').value = t.name;
            document.getElementById('tmpl-content').value = t.content;
            document.querySelector(`input[name="tmpl-channel"][value="${t.channel}"]`).checked = true;
            document.querySelector(`input[name="tmpl-status"][value="${t.status}"]`).checked = true;
        }
    } else {
        title.textContent = '新建模板';
        document.getElementById('tmpl-id').value = '';
    }

    modal.classList.add('show');
}

function closeTemplateModal() {
    document.getElementById('template-modal').classList.remove('show');
    editingTemplateId = null;
}

async function handleTemplateSubmit(e) {
    e.preventDefault();

    const formTemplateId = document.getElementById('tmpl-id').value.trim();

    const template = {
        name: document.getElementById('tmpl-name').value.trim(),
        content: document.getElementById('tmpl-content').value.trim(),
        channel: parseInt(document.querySelector('input[name="tmpl-channel"]:checked').value),
        status: parseInt(document.querySelector('input[name="tmpl-status"]:checked').value),
    };

    try {
        if (editingTemplateId) {
            // editingTemplateId 此时是 UUID（templateId），而不是数字 id
            template.templateId = editingTemplateId;
            await apiUpdateTemplate(template);
            showToast('模板更新成功', 'success');
        } else {
            await apiCreateTemplate(template);
            showToast('模板创建成功', 'success');
        }
        closeTemplateModal();
        loadTemplateList();
    } catch (err) {
        showToast(err.message || '保存失败', 'error');
    }
}

function editTemplate(templateId) {
    showTemplateModal(templateId);
}

async function deleteTemplate(templateId) {
    if (!confirm(`确定要删除模板 ${templateId} 吗？此操作不可恢复。`)) return;
    try {
        await apiDeleteTemplate(templateId);
        showToast('模板已删除', 'success');
        loadTemplateList();
    } catch (err) {
        showToast(err.message || '删除失败', 'error');
    }
}

// ============ 发送消息 ============
/**
 * 在发送表单中渲染模板列表，点击即选中
 */
function renderSendTemplateList() {
    const container = document.getElementById('send-template-list');
    if (!allTemplates || allTemplates.length === 0) {
        container.innerHTML = '<div style="color:#999;text-align:center;">暂无模板，请先在「模板管理」中创建</div>';
        return;
    }

    const channelName2 = ch => ({ 1: '📧 Email', 2: '📱 SMS', 3: '🦜 Lark' })[ch] || '未知';
    const statusName2 = s => ({ 1: '⏳ 待审', 2: '✅ 正常' })[s] || '未知';
    const statusColor = s => ({ 1: '#f0ad4e', 2: '#28a745' })[s] || '#999';

    const selected = document.getElementById('send-templateId').value;

    container.innerHTML = allTemplates.map(t => `
        <div class="send-tmpl-item ${t.templateId === selected ? 'active' : ''}"
             onclick="selectTemplateForSend('${t.templateId}')">
            <span class="send-tmpl-name">${t.name}</span>
            <span class="send-tmpl-ch">${channelName2(t.channel)}</span>
            <span class="send-tmpl-status" style="color:${statusColor(t.status)}">${statusName2(t.status)}</span>
            <code class="send-tmpl-id">${t.templateId.slice(0, 8)}…</code>
        </div>`).join('');
}

/**
 * 点击模板列表中的模板名，自动跳转到发送表单并填充该模板
 */
function selectTemplateForSend(templateId) {
    document.getElementById('send-templateId').value = templateId;
    document.getElementById('send-template-select').value = templateId;
    renderParamFields(templateId);
    renderTemplatePreview(templateId);
    renderSendTemplateList(); // 重新高亮选中项
    showToast('已选择模板，请填写收件人等信息', 'success');
}

function refreshTemplateSelector() {
    const select = document.getElementById('send-template-select');
    select.innerHTML = '<option value="">— 选择模板 —</option>';

    allTemplates.filter(t => t.status === 2).forEach(t => {
        const opt = document.createElement('option');
        opt.value = t.templateId;
        opt.textContent = `${t.name} (${channelName(t.channel)})`;
        select.appendChild(opt);
    });
}

function channelName(ch) {
    return { 1: 'Email', 2: 'SMS', 3: 'Lark' }[ch] || '未知';
}

function onTemplateSelectChange() {
    const select = document.getElementById('send-template-select');
    const input = document.getElementById('send-templateId');
    input.value = '';
    if (select.value) {
        input.value = select.value;
        renderParamFields(select.value);
        renderTemplatePreview(select.value);
    } else {
        clearParamFields();
        document.getElementById('send-template-preview').innerHTML = '<span class="preview-hint">请选择模板</span>';
    }
}

function onTemplateIdInput() {
    const select = document.getElementById('send-template-select');
    const input = document.getElementById('send-templateId');
    select.value = '';
    const id = input.value.trim();
    if (id) {
        renderParamFields(id);
        renderTemplatePreview(id);
    } else {
        clearParamFields();
        document.getElementById('send-template-preview').innerHTML = '<span class="preview-hint">请选择模板</span>';
    }
}

function renderTemplatePreview(templateId) {
    const t = allTemplates.find(x => x.templateId === templateId);
    const preview = document.getElementById('send-template-preview');
    if (t) {
        preview.innerHTML = `<pre style="margin:0;white-space:pre-wrap;word-break:break-all;">${escHtml(t.content)}</pre>`;
    } else {
        preview.innerHTML = '<span class="preview-hint">模板未找到</span>';
    }
}

function renderParamFields(templateId) {
    const t = allTemplates.find(x => x.templateId === templateId);
    const container = document.getElementById('param-fields');
    const keys = extractPlaceholders(t ? t.content : '');

    if (keys.length === 0) {
        container.innerHTML = '<div class="param-hint">该模板无需参数</div>';
        return;
    }

    const html = keys.map(key => `
        <div class="param-row">
            <span class="param-key">${key}</span>
            <input type="text" class="param-input" name="param_${key}" placeholder="请输入 ${key} 的值">
        </div>`).join('');
    container.innerHTML = html;
}

function clearParamFields() {
    document.getElementById('param-fields').innerHTML = '<div class="param-hint">请先选择模板，参数字段将自动生成</div>';
}

async function handleSendSubmit(e) {
    e.preventDefault();

    const templateId = document.getElementById('send-templateId').value.trim();
    if (!templateId) {
        showToast('请选择或输入模板 ID', 'warning');
        return;
    }

    const templateData = {};
    document.querySelectorAll('#param-fields input').forEach(input => {
        const key = input.name.replace('param_', '');
        if (key && input.value) {
            templateData[key] = input.value;
        }
    });

    const sendTimestamp = document.getElementById('send-timestamp-ms').value;
    const channelRadio = document.querySelector('input[name="channel"]:checked').value;
    const channel = parseInt(channelRadio);

    const msg = {
        templateId,
        sourceId: document.getElementById('send-sourceId').value.trim(),
        to: document.getElementById('send-to').value.trim(),
        subject: document.getElementById('send-subject').value.trim(),
        priority: parseInt(document.querySelector('input[name="priority"]:checked').value),
        channel: channel === 0 ? undefined : channel,
        templateData,
        sendTimestamp: sendTimestamp ? parseInt(sendTimestamp) : null,
    };

    if (!msg.sourceId) {
        showToast('请填写消息来源标识', 'warning');
        return;
    }
    if (!msg.to) {
        showToast('请填写收件人/手机号', 'warning');
        return;
    }

    try {
        const msgId = await apiSendMsg(msg);
        showToast(`消息已提交，msgId: ${msgId}`, 'success');

        // 添加到发送历史
        addSendHistory({
            msgId,
            templateId,
            to: msg.to,
            status: 'pending',
            time: new Date(),
        });

        // 清空参数但不重置模板选择
        document.querySelectorAll('#param-fields input').forEach(i => i.value = '');
        document.getElementById('send-to').value = '';
        document.getElementById('send-subject').value = '';
        document.getElementById('send-timestamp').value = '';
        document.getElementById('send-timestamp-ms').value = '';

    } catch (err) {
        showToast(err.message || '发送失败', 'error');
        addSendHistory({
            msgId: '—',
            templateId,
            to: msg.to,
            status: 'failed',
            time: new Date(),
            error: err.message,
        });
    }
}

function resetSendForm() {
    document.getElementById('send-form').reset();
    document.getElementById('send-templateId').value = '';
    document.getElementById('send-template-select').value = '';
    clearParamFields();
    document.getElementById('send-template-preview').innerHTML = '<span class="preview-hint">请选择模板</span>';
}

// ============ 发送历史 ============
function addSendHistory(item) {
    sendHistory.unshift(item);
    if (sendHistory.length > 50) sendHistory.pop();
    renderSendHistory();
}

function renderSendHistory() {
    const list = document.getElementById('send-history-list');
    if (sendHistory.length === 0) {
        list.innerHTML = '<div class="history-empty">暂无发送记录</div>';
        return;
    }

    const html = sendHistory.map(h => {
        const t = allTemplates.find(x => x.id === h.templateId);
        const tName = t ? t.templateName : `#${h.templateId}`;
        const timeStr = formatTime(h.time.getTime());
        const statusIcon = { pending: '⏳', success: '✅', failed: '❌' }[h.status] || '📋';
        const statusText = { pending: '待处理', success: '已提交', failed: '失败' }[h.status] || h.status;

        return `
            <div class="history-item ${h.status}">
                <div class="history-item-header">
                    <span class="history-item-id">${escHtml(h.msgId)}</span>
                    <span>${statusIcon} ${statusText}</span>
                </div>
                <div class="history-item-body">
                    <strong>${escHtml(tName)}</strong> → ${escHtml(h.to)}
                    ${h.error ? `<div style="color:var(--danger);font-size:12px;margin-top:4px;">${escHtml(h.error)}</div>` : ''}
                </div>
                <div class="history-item-time">${timeStr}</div>
            </div>`;
    }).join('');
    list.innerHTML = html;
}

function clearSendHistory() {
    sendHistory = [];
    renderSendHistory();
}

// ============ 发送记录查询 ============
async function handleRecordQuery(e) {
    e.preventDefault();
    const msgId = document.getElementById('record-msgId').value.trim();
    if (!msgId) {
        showToast('请输入消息 ID', 'warning');
        return;
    }

    const resultDiv = document.getElementById('record-result');
    resultDiv.innerHTML = '<div class="record-empty"><span>⏳</span><p>查询中...</p></div>';

    try {
        const record = await apiGetMsgRecord(msgId);
        renderRecordResult(record);
    } catch (err) {
        resultDiv.innerHTML = `
            <div class="record-empty">
                <span>❌</span>
                <p>未找到该消息记录</p>
                <p class="empty-hint">${err.message}</p>
            </div>`;
    }
}

function renderRecordResult(record) {
    if (!record) {
        document.getElementById('record-result').innerHTML = `
            <div class="record-empty"><span>❌</span><p>未找到记录</p></div>`;
        return;
    }

    const statusMap = {
        1: ['⏳ 待处理', 'badge-pending'],
        2: ['⚙️ 处理中', 'badge-processing'],
        3: ['✅ 成功', 'badge-normal'],
        4: ['❌ 失败', 'badge-failed'],
    };
    const channelMap = { 1: ['📧 Email', 'channel-email'], 2: ['📱 SMS', 'channel-sms'], 3: ['🦜 Lark', 'channel-lark'] };

    const [stLabel, stClass] = statusMap[record.status] || ['未知', ''];
    const [chLabel, chClass] = channelMap[record.channel] || ['未知', ''];

    const html = `
        <div class="record-info-grid">
            <div class="record-info-item">
                <label>消息 ID</label>
                <span><code>${escHtml(record.id || record.msgId || '—')}</code></span>
            </div>
            <div class="record-info-item">
                <label>发送状态</label>
                <span class="badge ${stClass}">${stLabel}</span>
            </div>
            <div class="record-info-item">
                <label>渠道</label>
                <span class="channel-tag ${chClass}">${chLabel}</span>
            </div>
            <div class="record-info-item">
                <label>重试次数</label>
                <span>${record.retryCount ?? 0} / 5</span>
            </div>
        </div>
        <div class="record-full">
            <div class="record-full-item">
                <label>模板 ID</label>
                <div class="value"><code>${record.templateId || '—'}</code></div>
            </div>
            <div class="record-full-item">
                <label>来源</label>
                <div class="value">${escHtml(record.sourceId || '—')}</div>
            </div>
            ${record.errorMsg ? `
            <div class="record-full-item">
                <label>错误信息</label>
                <div class="value" style="color:var(--danger);">${escHtml(record.errorMsg)}</div>
            </div>` : ''}
            <div class="record-full-item">
                <label>创建时间</label>
                <div class="value">${formatTime(record.createTime) || '—'}</div>
            </div>
            <div class="record-full-item">
                <label>更新时间</label>
                <div class="value">${formatTime(record.modifyTime) || '—'}</div>
            </div>
        </div>
        <div style="margin-top:16px;display:flex;gap:8px;">
            <button class="btn btn-secondary" onclick="reQueryRecord('${record.id || record.msgId}')">🔄 重新查询</button>
        </div>`;

    document.getElementById('record-result').innerHTML = html;
}

function reQueryRecord(msgId) {
    document.getElementById('record-msgId').value = msgId;
    document.getElementById('record-form').dispatchEvent(new Event('submit'));
}

// ============ 工具函数 ============
function escHtml(str) {
    if (!str) return '';
    const div = document.createElement('div');
    div.textContent = str;
    return div.innerHTML;
}
