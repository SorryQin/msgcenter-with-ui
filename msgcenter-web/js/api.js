/**
 * MsgCenter API 客户端
 * 所有与后端的 HTTP 通信都通过这里
 */

const API_BASE = 'http://localhost:8082/msg';

/**
 * 通用请求封装
 */
async function request(method, path, body = null) {
    const options = {
        method,
        headers: { 'Content-Type': 'application/json' },
    };
    if (body) {
        options.body = JSON.stringify(body);
    }

    const response = await fetch(API_BASE + path, options);
    const json = await response.json();

    if (json.code !== 0) {
        throw new Error(json.message || '请求失败');
    }
    return json.data;
}

/* ==================== 模板管理 ==================== */

/**
 * 创建模板
 * @param {Object} template - { templateName, templateContent, channel, status }
 */
async function apiCreateTemplate(template) {
    return await request('POST', '/create_template', template);
}

/**
 * 获取模板详情
 * @param {string|number} templateId
 */
async function apiGetTemplate(templateId) {
    return await request('GET', `/get_template?templateId=${templateId}`);
}

/**
 * 更新模板
 * @param {Object} template - { id, templateName, templateContent, channel, status }
 */
async function apiUpdateTemplate(template) {
    return await request('POST', '/update_template', template);
}

/**
 * 删除模板
 * @param {string|number} templateId
 */
async function apiDeleteTemplate(templateId) {
    return await request('POST', `/del_template?templateId=${templateId}`);
}

/**
 * 获取所有模板列表
 */
async function apiFindAllTemplate() {
    return await request('GET', '/find_all_template');
}

/* ==================== 消息发送 ==================== */

/**
 * 发送消息
 * @param {Object} msg - { templateId, sourceId, to, subject, priority, channel, templateData, sendTimestamp }
 */
async function apiSendMsg(msg) {
    return await request('POST', '/send_msg', msg);
}

/* ==================== 发送记录 ==================== */

/**
 * 查询发送记录
 * @param {string} msgId
 */
async function apiGetMsgRecord(msgId) {
    return await request('GET', `/get_msg_record?msgId=${msgId}`);
}

/* ==================== 通用工具 ==================== */

/**
 * 健康检查（发一个简单请求看服务是否存活）
 */
async function apiHealthCheck() {
    try {
        await fetch(API_BASE + '/get_template?templateId=99999999', {
            method: 'GET',
        });
        return true;
    } catch (e) {
        return false;
    }
}

/**
 * 格式化时间
 */
function formatTime(timestamp) {
    if (!timestamp) return '—';
    const d = new Date(timestamp);
    const pad = n => String(n).padStart(2, '0');
    return `${d.getFullYear()}-${pad(d.getMonth()+1)}-${pad(d.getDate())} ` +
           `${pad(d.getHours())}:${pad(d.getMinutes())}:${pad(d.getSeconds())}`;
}

/**
 * 格式化日期（datetime-local input 用）
 */
function formatDateTimeLocal(timestamp) {
    if (!timestamp) return '';
    const d = new Date(timestamp);
    const pad = n => String(n).padStart(2, '0');
    return `${d.getFullYear()}-${pad(d.getMonth()+1)}-${pad(d.getDate())}T` +
           `${pad(d.getHours())}:${pad(d.getMinutes())}`;
}

/**
 * 从模板内容中提取所有占位符 key
 * @param {string} content - 模板内容
 * @returns {string[]} - 占位符 key 数组，如 ["username", "orderId"]
 */
function extractPlaceholders(content) {
    if (!content) return [];
    const matches = content.match(/\$\{([^}]+)\}/g) || [];
    return matches.map(m => m.slice(2, -1));
}
