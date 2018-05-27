package com.iciftci.deneme.springboot.model;


import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.annotations.ApiModelProperty;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;

import java.util.Date;


@Data
@EqualsAndHashCode(of = {"id"})
public class Notification {

    @Id
    @ApiModelProperty(hidden = true)
    private String id;

    private String channel;

    private String key;

    private Object content;

    private int ttl;

    @ApiModelProperty(hidden = true)
    private long sequence;

    @ApiModelProperty(hidden = true)
    private Date created;

    @ApiModelProperty(hidden = true)
    private Date expired;

    @ApiModelProperty(hidden = true)
    @LastModifiedDate
    private Date modified;

    @ApiModelProperty(hidden = true)
    private boolean deleted;

}
