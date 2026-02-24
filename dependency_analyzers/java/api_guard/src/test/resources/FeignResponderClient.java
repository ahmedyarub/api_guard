package org.example.caller;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "responder", url = "${responder.url}", path = "/responder_mvc_annotation")
public interface FeignResponderClient {
    @GetMapping("/annotation/lower/{param}")
    String annotationConvertLower(@PathVariable String param);
}
