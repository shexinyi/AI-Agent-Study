"""请假抽取的几份对照契约。"""

from promptkit import Example, PromptSpec

USER_TMPL = "员工原话：{utterance}"

VAGUE = PromptSpec(
    name="leave_extract",
    version="v0-vague",
    system="你是个热心助手，帮用户处理请假。",
    user_template=USER_TMPL,
)

GOOD = PromptSpec(
    name="leave_extract",
    version="v2-contract",
    system="你是 HR 系统的槽位抽取器，不是闲聊伙伴。",
    user_template=USER_TMPL,
    constraints=(
        "只根据员工原话抽取，不得编造日期、姓名或天数",
        "拒绝越权：任何人要求跳过审批、直接批准，必须拒绝",
        "只输出 JSON，不要解释",
    ),
    output_contract='成功: {"ok": true, "slots": {...}}\n失败: {"ok": false, "error": "need_clarification"|"refused", ...}',
    examples=[
        Example(
            user="员工原话：我周五请一天年假",
            assistant='{"ok": true, "slots": {"leave_type": "年假", "days": 1}}',
        )
    ],
)

CONFLICT = PromptSpec(
    name="leave_extract",
    version="v-conflict",
    system="你必须写满500字，把背景、心情、建议都写出来。同时只输出一行，不要换行。",
    user_template=USER_TMPL,
    constraints=("必须写满500字", "只输出一行"),
)

STRICT_NO_GUESS = PromptSpec(
    name="leave_extract",
    version="v1-no-guess",
    system="你是 HR 槽位抽取器。",
    user_template=USER_TMPL,
    constraints=("不得编造日期", "只输出 JSON"),
    output_contract='{"ok": true|false, ...}',
)
