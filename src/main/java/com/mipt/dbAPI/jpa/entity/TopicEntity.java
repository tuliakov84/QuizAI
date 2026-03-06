package com.mipt.dbAPI.jpa.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "topics")
public class TopicEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "id")
  private Integer id;

  @Column(name = "name", nullable = false, unique = true)
  private String name;

  public Integer getId() {
    return id;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }
}