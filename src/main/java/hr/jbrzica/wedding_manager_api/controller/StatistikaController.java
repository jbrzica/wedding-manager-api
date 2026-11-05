package hr.jbrzica.wedding_manager_api.controller;

import hr.jbrzica.wedding_manager_api.dto.StatistikaResponse;
import hr.jbrzica.wedding_manager_api.service.StatistikaService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/statistika")
public class StatistikaController {

    private final StatistikaService statistikaService;

    public StatistikaController(StatistikaService statistikaService) {
        this.statistikaService = statistikaService;
    }

    @GetMapping
    public StatistikaResponse statistika(@RequestParam int godina) {
        return statistikaService.za(godina);
    }
}
