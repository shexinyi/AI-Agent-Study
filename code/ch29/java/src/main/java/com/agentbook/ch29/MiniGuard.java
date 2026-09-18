package com.agentbook.ch29;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** 安全教具。身份来自会话；检索是资料；工具看白名单。 */
public final class MiniGuard {
    public static final Map<String, Role> ROLES = Map.of(
            "intern", new Role(List.of("search_policy", "get_order"), List.of("public")),
            "hr", new Role(List.of("search_policy", "get_order"), List.of("public", "hr")),
            "agent_ops", new Role(List.of("search_policy", "get_order", "refund_order"), List.of("public", "hr"))
    );

    public static final class Identity {
        public final String user;
        public final String role;
        public final String tenant;

        public Identity(String user, String role, String tenant) {
            this.user = user;
            this.role = role;
            this.tenant = tenant;
        }
    }

    public static final class Doc {
        public final String id;
        public final String text;
        public final String acl;
        public final String tenant;
        public final boolean deprecated;
        public final boolean untrusted;

        public Doc(String id, String text, String acl, String tenant, boolean deprecated, boolean untrusted) {
            this.id = id;
            this.text = text;
            this.acl = acl;
            this.tenant = tenant;
            this.deprecated = deprecated;
            this.untrusted = untrusted;
        }
    }

    public static final class Decision {
        public final boolean allow;
        public final String reason;
        public final String tool;

        public Decision(boolean allow, String reason, String tool) {
            this.allow = allow;
            this.reason = reason;
            this.tool = tool;
        }
    }

    public static final class Role {
        public final List<String> tools;
        public final List<String> acl;

        public Role(List<String> tools, List<String> acl) {
            this.tools = tools;
            this.acl = acl;
        }
    }

    public static final class Result {
        public String text = "";
        public final List<String> tools = new ArrayList<>();
        public final List<String> executed = new ArrayList<>();
        public final List<String> seenIds = new ArrayList<>();
        public final List<Decision> decisions = new ArrayList<>();
    }

    public static Decision allowTool(Identity ident, String tool) {
        Role role = ROLES.get(ident.role);
        if (role == null || !role.tools.contains(tool)) {
            return new Decision(false, ident.role + " 不在 " + tool + " 白名单", tool);
        }
        return new Decision(true, "白名单通过", tool);
    }

    public static Decision allowDoc(Identity ident, Doc doc, boolean applyAcl) {
        if (!doc.tenant.equals(ident.tenant)) {
            return new Decision(false, "跨租户", doc.id);
        }
        if (applyAcl && !ROLES.get(ident.role).acl.contains(doc.acl)) {
            return new Decision(false, "ACL " + doc.acl + " 对 " + ident.role + " 不可见", doc.id);
        }
        return new Decision(true, "可见", doc.id);
    }

    public static final class PolicyEngine {
        public final List<Doc> catalog;

        public PolicyEngine(List<Doc> catalog) {
            this.catalog = catalog;
        }

        public List<Doc> retrieve(Identity ident, boolean applyAcl) {
            List<Doc> seen = new ArrayList<>();
            for (Doc doc : catalog) {
                if (allowDoc(ident, doc, applyAcl).allow) {
                    seen.add(doc);
                }
            }
            return seen;
        }

        public Decision checkRefund(Identity ident, boolean hitlApproved) {
            Decision gate = allowTool(ident, "refund_order");
            if (!gate.allow) {
                return gate;
            }
            if (!hitlApproved) {
                return new Decision(false, "写工具须 HITL", "refund_order");
            }
            return new Decision(true, "HITL 已批准", "refund_order");
        }
    }
}
