package com.example.artinus.external;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@FeignClient(name = "csrngClient", url = "${external.csrng.url}")
public interface CsrngFeignClient {

    @GetMapping("/csrng/csrng.php")
    List<CsrngResponse> getRandomNumber(@RequestParam("min") int min, @RequestParam("max") int max);

    record CsrngResponse(String status, int min, int max, int random) {}
}
