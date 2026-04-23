package com.mipt.service;

import com.mipt.dbAPI.DbService;
import jakarta.annotation.PostConstruct;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
public class PremiumExpirationService {
  private final DbService dbService;

  public PremiumExpirationService(DbService dbService) {
    this.dbService = dbService;
  }

  @PostConstruct
  public void expireOnStartup() {
    expireExpiredPremiumBenefits();
  }

  @Scheduled(fixedRate = 1_000)
  public void expireExpiredPremiumBenefits() {
    dbService.expireExpiredPremiumBenefits();
  }
}
