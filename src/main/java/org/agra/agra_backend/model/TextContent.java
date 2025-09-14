package org.agra.agra_backend.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class TextContent {
    @Id
    private String id;

    private String title;
    private String content;
    private int order;
    private String type;

}
