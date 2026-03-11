package com.mipt.api;

import com.mipt.dbAPI.DbService;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;


@RestController
public class TestRestController {
  private final DbService dbService;

  public TestRestController(DbService dbService) {
    this.dbService = dbService;
  }

  @GetMapping("/hello-rest")
  public String test(@RequestParam(required = false, defaultValue = "World!") String name, Model model) {
    return "hello " + name;
  }

  @GetMapping("/test-db-rest")
  public String testDb(Model model) {
    try {
      // проверка подключения к базе данных
      dbService.getGlobalLeaderboards();
      return "Connection is successful";
    } catch (Exception e) {
      return "Error: " + e.getMessage();
    }
  }
}
