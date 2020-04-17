package com.tiddar.rafasync.domain;

import lombok.Data;
import org.springframework.data.annotation.Id;

import javax.annotation.Generated;

@Data
public class Log {
    @Id
    Integer index;
    String command;

}
