package org.cloudburstmc.protocol.bedrock.codec.v818.serializer;

import org.cloudburstmc.protocol.bedrock.codec.v291.serializer.LoginSerializer_v291;
import org.cloudburstmc.protocol.bedrock.data.auth.AuthPayload;
import org.cloudburstmc.protocol.bedrock.data.auth.AuthType;
import org.cloudburstmc.protocol.bedrock.data.auth.CertificateChainPayload;
import org.cloudburstmc.protocol.bedrock.data.auth.TokenPayload;
import org.jose4j.json.JsonUtil;
import org.jose4j.lang.JoseException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static org.cloudburstmc.protocol.common.util.Preconditions.checkArgument;

public class LoginSerializer_v818 extends LoginSerializer_v291 {

    public static final LoginSerializer_v818 INSTANCE = new LoginSerializer_v818();

    @Override
    protected String writeAuthJwt(AuthPayload payload) {
        Objects.requireNonNull(payload, "AuthPayload cannot be null");
        checkArgument(payload.getAuthType() != null && payload.getAuthType() != AuthType.UNKNOWN,
                "Client requires non-null and non-UNKNOWN AuthType for login");
        Map<String, Object> object = new HashMap<>();
        object.put("AuthenticationType", payload.getAuthType().ordinal() - 1); // Adjusting ordinal to match the enum definition
        if (payload instanceof TokenPayload) {
            object.put("Token", ((TokenPayload) payload).getToken());
            object.put("Certificate", "");
        } else if (payload instanceof CertificateChainPayload) {
            Map<String, Object> json = new HashMap<>();
            json.put("chain", ((CertificateChainPayload) payload).getChain());
            object.put("Certificate", JsonUtil.toJson(json));
            object.put("Token", "");
        } else {
            throw new IllegalArgumentException("Unsupported AuthPayload type: " + payload.getClass().getName());
        }
        return JsonUtil.toJson(object);
    }

    @Override
    protected AuthPayload readAuthJwt(String authJwt) {
        try {
            Map<String, Object> payload = JsonUtil.parseJson(authJwt);
            checkArgument(payload.containsKey("AuthenticationType"), "Missing AuthenticationType in JWT");
            int authTypeOrdinal = ((Number) payload.get("AuthenticationType")).intValue();
            if (authTypeOrdinal < 0 || authTypeOrdinal >= AuthType.values().length - 1) {
                throw new IllegalArgumentException("Invalid AuthenticationType ordinal: " + authTypeOrdinal);
            }
            AuthType authType = AuthType.values()[authTypeOrdinal + 1];

            if (payload.containsKey("Certificate") && payload.get("Certificate") instanceof String && !((String) payload.get("Certificate")).isEmpty()) {
                String certJson = (String) payload.get("Certificate");
                Map<String, Object> certData = JsonUtil.parseJson(certJson);
                if (!certData.containsKey("chain") || !(certData.get("chain") instanceof List)) {
                    throw new IllegalArgumentException("Invalid Certificate chain in JWT");
                }
                List<String> chain = (List<String>) certData.get("chain");
                return new CertificateChainPayload(chain, authType);
            } else if (payload.containsKey("Token") && payload.get("Token") instanceof String && !((String) payload.get("Token")).isEmpty()) {
                String token = (String) payload.get("Token");
                return new TokenPayload(token, authType);
            } else {
                throw new IllegalArgumentException("Invalid AuthPayload in JWT");
            }
        } catch (JoseException e) {
            throw new IllegalArgumentException("Failed to parse auth payload", e);
        }
    }
}
