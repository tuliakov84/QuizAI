package com.mipt;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class MainTest {

  @Test
  void testClass() {
    var example = new testClass();
    assertEquals(30, example.testMethodSum(20));
  }
}
