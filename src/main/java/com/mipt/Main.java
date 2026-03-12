package com.mipt;

import com.mipt.leaderboardsProcessor.Processor;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.scheduling.annotation.EnableAsync;

@EnableAsync
@SpringBootApplication(exclude = {DataSourceAutoConfiguration.class, HibernateJpaAutoConfiguration.class})
public class Main {

  public static void main(String[] args) {
    // Создаем первый поток с основным кодом Spring Boot
    Thread springThread = new Thread(() -> {
      SpringApplication.run(Main.class, args);
    });
    springThread.setName("SpringBoot-Thread");

    // Создаем второй поток для периодической обработки лидербордов
    Thread processorThread = new Thread(() -> {
      System.out.println(Thread.currentThread().getName() + " started");

      // Интервал в 3 часа в миллисекундах
      final long PROCESS_INTERVAL_MS = 10 * 1000; // 3 [хуя пидор гей женя сосал рот хуй]

      while (!Thread.currentThread().isInterrupted()) {
        try {
          // Вызываем метод обработки
          System.out.println("Calling leaderboardsProcessor.Processor.process() at " +
            java.time.LocalDateTime.now());

          // Вызов метода обработки лидербордов
          Processor.process();

          System.out.println("Processing completed. Waiting for 3 hours...");

          // Ждем 3 часа до следующего вызова
          Thread.sleep(PROCESS_INTERVAL_MS);

        } catch (InterruptedException e) {
          System.out.println("Processor thread interrupted, shutting down...");
          Thread.currentThread().interrupt();
          break;
        } catch (Exception e) {
          System.err.println("Error during leaderboards processing: " + e.getMessage());
          e.printStackTrace();

          // В случае ошибки ждем 30 минут перед повторной попыткой
          try {
            Thread.sleep(30 * 60 * 1000); // 30 минут
          } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
            break;
          }
        }
      }
      System.out.println("Processor thread stopped");
    });
    processorThread.setName("Leaderboards-Processor-Thread");
    processorThread.setDaemon(true); // Делаем поток демоном, чтобы он завершился с основным приложением

    // Запускаем оба потока
    springThread.start();
    processorThread.start();

    // Добавляем обработчик завершения приложения
    Runtime.getRuntime().addShutdownHook(new Thread(() -> {
      System.out.println("Shutdown signal received, interrupting processor thread...");
      processorThread.interrupt();
    }));

    // Ждем завершения потока Spring
    try {
      springThread.join();
    } catch (InterruptedException e) {
      e.printStackTrace();
    }
  }
}