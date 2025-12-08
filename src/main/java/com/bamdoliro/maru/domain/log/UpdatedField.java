package com.bamdoliro.maru.domain.log;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "tbl_updated_field")
@Entity
public class UpdatedField {

    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Id
    private Long id;

    @Column(nullable = false)
    private String filedName;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String oldValue;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String newValue;

    @ManyToOne(fetch = FetchType.LAZY)
    @OnDelete(action = OnDeleteAction.CASCADE)
    @JoinColumn(nullable = false, name = "log_id")
    private FormUpdateLog formUpdateLog;

    @Builder
    public UpdatedField(String filedName, String oldValue, String newValue, FormUpdateLog formUpdateLog) {
        this.filedName = filedName;
        this.oldValue = oldValue;
        this.newValue = newValue;
        this.formUpdateLog = formUpdateLog;
    }
}
