package dev.kodobo.crdtgraphchallenge.helpers;

import org.springframework.util.IdGenerator;

import java.util.UUID;

public class FixedIdGenerator implements IdGenerator {
    @Override
    public UUID generateId() {
        return generateId("fixedId");
    }

    public UUID generateId(String string) {
        return UUID.nameUUIDFromBytes(string.getBytes());
    }

    public UUID generateId(UUID uid1, UUID uid2) {
        byte[] bytes = uid1.toString().concat(uid2.toString()).getBytes();
        return  UUID.nameUUIDFromBytes(bytes);
    }
}
