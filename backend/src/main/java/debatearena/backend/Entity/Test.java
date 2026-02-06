package debatearena.backend.Entity;

import jakarta.persistence.*;

@Entity
public class Test {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Integer note;

    // getters et setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Integer getNote() { return note; }
    public void setNote(Integer note) { this.note = note; }
}
