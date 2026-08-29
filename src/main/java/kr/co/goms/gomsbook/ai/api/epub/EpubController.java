package kr.co.goms.gomsbook.ai.api.epub;

import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class EpubController {

    @GetMapping("/epub")
    public Map<String, Object> health() {

        Map<String, Object> result = new LinkedHashMap<>();

        result.put("status", "UP");

        result.put("service", "gomsbook-ai-api");

        return result;

    }

}