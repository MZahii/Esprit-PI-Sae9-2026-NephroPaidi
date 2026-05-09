package tn.esprit.spring.clinicalservice;


import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/ms2/clinical")

public class ClinicalServiceRestApi {

    @GetMapping("/hello")
    public String sayHello(){
        return "Hello Im ms2";
    }
}
