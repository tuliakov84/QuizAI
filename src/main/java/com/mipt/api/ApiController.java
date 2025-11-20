package com.mipt.api;

import com.mipt.dbAPI.DatabaseAccessException;
import com.mipt.dbAPI.DbService;
import com.mipt.domainModel.Game;
import com.mipt.domainModel.User;
import com.mipt.utils.BackendUtils;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.sql.SQLException;


@RestController
@RequestMapping("/api")
public class ApiController {
  private static final int MAX_SESSION_RETRIES = 5;

  private final BackendUtils utils;
  private final DbService dbService;
  public ApiController(DbService dbService) {
    this.dbService = dbService;
    this.utils = new BackendUtils(dbService);
  }

  @PostMapping("/auth/login")
  public ResponseEntity<Object> auth(@RequestBody User user) {
    for (int attempt = 0; attempt < MAX_SESSION_RETRIES; attempt++) {
      String session = utils.generateSessionId();
      try {
        dbService.authenticate(user.getUsername(), user.getPassword(), session);
        user.setSession(session);
        user.setUserId(dbService.getUserId(session));
        Integer currentGame = dbService.getCurrentGame(session);
        if (currentGame != null) {
          Game game = new Game();
          game.setGameId(currentGame);
          user.setCurrentGame(game);
        }
        user.setDescription(dbService.getDescription(session));
        user.setGlobalPoints(dbService.getGlobalPoints(session));
        return new ResponseEntity<>(user, HttpStatus.OK);
      } catch (DatabaseAccessException e) {
        if (utils.isSessionCollision(e)) {
          continue;
        }
        return new ResponseEntity<>("Failed to authenticate user '" + user.getUsername() + "': " + e.getMessage(), HttpStatus.NOT_FOUND);
      } catch (SQLException e) {
        return new ResponseEntity<>("Database error occurred while authenticating user '" + user.getUsername() + "'", HttpStatus.INTERNAL_SERVER_ERROR);
      }
    }
    return new ResponseEntity<>("Unable to generate unique session token", HttpStatus.INTERNAL_SERVER_ERROR);
  }

  @PostMapping("/auth/register")
  public ResponseEntity<Object> register(@RequestBody User user) {
    try {
      dbService.register(user.getUsername(), user.getPassword());
      return auth(user);
    } catch (DatabaseAccessException e) {
      return new ResponseEntity<>("Failed to register user '" + user.getUsername() + "': " + e.getMessage(), HttpStatus.NOT_FOUND);
    } catch (SQLException e) {
      return new ResponseEntity<>("Database error occurred while registering user '" + user.getUsername() + "'", HttpStatus.NOT_FOUND);
    }
  }

  @PostMapping("/auth/logout")
  public ResponseEntity<Object> logout(@RequestBody User user) {
    try {
      dbService.logOut(user.getSession());
      return new ResponseEntity<>(HttpStatus.OK);
    } catch (DatabaseAccessException e) {
      return new ResponseEntity<>("Failed to log out of account '" + user.getUsername() + "': " + e.getMessage(), HttpStatus.NOT_FOUND);
    } catch (SQLException e) {
      return new ResponseEntity<>("Database error occurred while logging out of account '" + user.getUsername() + "'", HttpStatus.NOT_FOUND);
    }
  }
}
