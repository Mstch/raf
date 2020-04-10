package com.tiddar.raf.domain;

import lombok.Data;

import javax.persistence.*;

@Data
@Entity
public class Log {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    Integer index;
    String command;

}
