package com.example.demo.model;

import lombok.*;
import lombok.experimental.Accessors;

import javax.persistence.*;

@Entity
@Getter @Setter
@Builder
@Accessors(chain = true)
@AllArgsConstructor
@NoArgsConstructor
@ToString(exclude = {"board","file"})
public class Plant {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "plant_id")
    private Long id;
    @Enumerated(EnumType.STRING)
    private PlantCategoryEnum plantCategory;
    private String name;

    @OneToOne(mappedBy = "plant",fetch = FetchType.LAZY)
    private Board board;

    @OneToOne(mappedBy = "plant",fetch = FetchType.LAZY)
    private File file;


}
