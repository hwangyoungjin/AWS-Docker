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
@ToString(exclude = {"board","plant"})
public class File {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "file_id")
    private Long id;
    private String url;

    @Enumerated(EnumType.STRING)
    private FileTypeEnum fileType;

    @Enumerated(EnumType.STRING)
    private PlaceEnum place;

    @Enumerated(EnumType.STRING)
    private PlantLifeEnum plantLife;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "board_id")
    private Board board;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "plant_id")
    private Plant plant;

    //board
    public void setBoard(Board board) {
        this.board = board;
        board.getFiles().add(this);
    }

    //plant
    public void setPlant(Plant plant) {
        this.plant = plant;
        plant.setFile(this);
    }
}
