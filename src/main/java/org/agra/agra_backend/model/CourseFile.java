package org.agra.agra_backend.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;

import java.util.Date;
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class CourseFile {

        @Id
        private String id;

        private String name;
        private String type; // pdf, doc, ppt, etc.
        private String url;
        private String publicId;
        private long size;
        private Date uploadDate;
}
