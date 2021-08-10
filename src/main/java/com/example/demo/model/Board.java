package com.example.demo.model;

import lombok.*;
import lombok.experimental.Accessors;

import javax.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Setter
@Getter
@Builder
@Accessors(chain = true)
@AllArgsConstructor
@NoArgsConstructor
@ToString(exclude = {"plant","writer","files","comments"})
public class Board {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "board_id")
    private Long id;

    private String title;
    private String subTile;
    private String content;
    private int view;
    private int likeNumber;
    private int share;
    private LocalDateTime regDate;
    private LocalDateTime updateDate;

    @Enumerated(EnumType.STRING)
    private PlantLifeEnum plantLife;

    @Enumerated(EnumType.STRING)
    private PlaceEnum place;

    @Enumerated(EnumType.STRING)
    private BoardTypeEnum boardType;
    private boolean visible; //True : stored , False : temporary

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "plant_id")
    private Plant plant;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "writer_id")
    private Account writer;

    @Builder.Default
    @OneToMany(mappedBy = "board",fetch = FetchType.LAZY)
    private List<File> files = new ArrayList<>();

    @Builder.Default
    @OneToMany(mappedBy = "board",fetch = FetchType.LAZY)
    private List<Comment> comments = new ArrayList<>();

    //plant
    public void setPlant(Plant plant) {
        this.plant = plant;
        plant.setBoard(this);
    }

    //account
    public void setAccount(Account account) {
        this.writer = account;
        account.getBoards().add(this);
    }

    //생성, 저장, 수정, 삭제

    //==== 비즈니스 로직 ====
    public static Board CreateBoard() {
        Board board = new Board();
        return board;
    }

}
