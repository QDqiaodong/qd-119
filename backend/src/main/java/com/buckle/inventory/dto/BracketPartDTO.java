package com.buckle.inventory.dto;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class BracketPartDTO extends BucklePartDTO {

    private Integer length;
    private Integer holeSpacing;
}
