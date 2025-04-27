package dev.group.cybershield.common;

import lombok.Data;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Data
@Component
public class LocalCache {
    private Map<String,Object> cached = new HashMap<>();
}
