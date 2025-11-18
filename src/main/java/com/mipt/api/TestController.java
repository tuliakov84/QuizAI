package com.mipt.api;

import com.mipt.dbAPI.DbService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;


@Controller
public class TestController {
  private final DbService dbService;

  public TestController(DbService dbService) {
    this.dbService = dbService;
  }

  @GetMapping("/hello")
  public String test(@RequestParam(required = false, defaultValue = "World!") String name, Model model) {
    model.addAttribute("name", name);
    return "hello";
  }

  @GetMapping("/test-db")
  public String testDb(Model model) {
    try {
      // проверка подключения к базе данных
      dbService.getGlobalLeaderboards();
      model.addAttribute("status", "Successful!");
    } catch (Exception e) {
      model.addAttribute("status", "With error: " + e.getMessage());
    }
    return "dbConnStatus";
  }
}
