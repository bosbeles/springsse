package com.iciftci.deneme.springboot.model;


import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;
import java.util.Date;


@Data
@EqualsAndHashCode(of = {"id"})
public class Notification {

    public static final int ACTIVE = 0;
    public static final int DELETED = 1;
    public static final int EXPIRED = 2;

    @Id
    @ApiModelProperty(hidden = true)
    private String id;

    private String channel;

    private String tag;

    private Object content;

    private int ttl;

    @ApiModelProperty(hidden = true)
    private Date created;

    @ApiModelProperty(hidden = true)
    private Date expired;

    @ApiModelProperty(hidden = true)
    @LastModifiedDate
    private Date modified;

    @ApiModelProperty(hidden = true)
    private int state;

}
