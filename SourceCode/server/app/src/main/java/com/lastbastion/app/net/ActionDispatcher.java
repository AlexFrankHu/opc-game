package com.lastbastion.app.net;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.lastbastion.app.ActionRegistry;
import com.lastbastion.common.ErrorCode;
import com.lastbastion.common.GameException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

/**
 * 将收到的 JSON 帧分发到注册的 {@link ActionHandler}。
 * 帧格式（对齐客户端 NetClient）：
 *  request:  {"id":1,"cmd":2,"subCmd":4,"action":"survivor.pullGacha","payload":{...}}
 *  response: {"id":1,"ok":true,"data":{...}}
 *  error:    {"id":1,"ok":false,"error":"...","code":"ERR_CODE"}
 *
 * 为了向后兼容，既接受 `action` 字符串，也接受 `cmd+subCmd` 数字键。
 */
public final class ActionDispatcher {

    private static final Logger log = LoggerFactory.getLogger(ActionDispatcher.class);

    private final ObjectMapper mapper = new ObjectMapper();
    private final Map<String, ActionHandler> byName = new HashMap<>();
    private final Map<Integer, ActionHandler> byCode = new HashMap<>();
    private SessionRegistry sessions;

    public void setSessionRegistry(SessionRegistry registry) { this.sessions = registry; }

    public void register(ActionHandler handler) {
        byName.put(handler.name(), handler);
        Integer code = ActionRegistry.ALL.get(handler.name());
        if (code != null) byCode.put(code, handler);
    }

    public int size() { return byName.size(); }

    public String dispatchRaw(Session session, String json) {
        try {
            JsonNode root = mapper.readTree(json);
            int id = root.path("id").asInt(0);
            ActionHandler h = lookup(root);
            if (h == null) {
                return errorResp(id, "UNKNOWN_ACTION", "no handler for " + root);
            }
            if (h.requiresLogin() && !session.isLoggedIn()) {
                return errorResp(id, "NOT_LOGGED_IN", "please call user.login first");
            }
            Object data;
            try {
                data = h.handle(session, root.path("payload"));
            } catch (GameException ge) {
                return errorResp(id, ge.errorCode().name(), ge.getMessage());
            } catch (Exception e) {
                log.error("handler {} failed", h.name(), e);
                return errorResp(id, ErrorCode.INTERNAL.name(), e.getMessage());
            }
            // 业务执行成功 → 写回快照（persist-on-each-action）。
            if (sessions != null && session.isLoggedIn()) {
                try {
                    sessions.save(session.player().externalId(), session.player());
                } catch (Exception ex) {
                    log.warn("auto-save failed for player {}", session.player().playerId(), ex);
                }
            }
            ObjectNode resp = mapper.createObjectNode();
            resp.put("id", id);
            resp.put("ok", true);
            resp.set("data", mapper.valueToTree(data));
            return mapper.writeValueAsString(resp);
        } catch (Exception e) {
            log.error("bad frame", e);
            return errorResp(0, "BAD_FRAME", e.getMessage());
        }
    }

    private ActionHandler lookup(JsonNode root) {
        JsonNode nameNode = root.path("action");
        if (!nameNode.isMissingNode() && !nameNode.isNull()) {
            ActionHandler h = byName.get(nameNode.asText());
            if (h != null) return h;
        }
        int cmd = root.path("cmd").asInt(-1);
        int sub = root.path("subCmd").asInt(-1);
        if (cmd >= 0 && sub >= 0) {
            return byCode.get((cmd << 16) | sub);
        }
        return null;
    }

    private String errorResp(int id, String code, String msg) {
        try {
            ObjectNode n = mapper.createObjectNode();
            n.put("id", id);
            n.put("ok", false);
            n.put("code", code);
            n.put("error", msg);
            return mapper.writeValueAsString(n);
        } catch (Exception e) {
            return "{\"ok\":false,\"error\":\"serialization failed\"}";
        }
    }
}
