package org.mabera.jasper.controller;

import java.util.HashMap;
import java.util.Map;

import org.mabera.jasper.main.Main;
import org.mabera.jasper.model.GenericResponse;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RequestMapping("api/jasper")
@CrossOrigin(origins = "http://localhost")
@RestController
public class ApiController {
    Main main = new Main();

    @PostMapping("http")
    public GenericResponse<String> placeOrder(@RequestBody Map<String, String> request) {

        Main.apiRun(request,"http");        
        GenericResponse<String> resp = GenericResponse.<String>builder()
                .success(true)
                .msg("Report Dispatched successfully")
                .build();
        return resp;
    }

}
