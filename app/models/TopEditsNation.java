package models;

import java.util.*;
import javax.persistence.*;

import play.db.ebean.*;
import play.data.format.*;
import play.data.validation.*;

@Entity
public class TopEditsNation extends Model {

    @Id
    public Long id;

    @Constraints.Required
    public String isoCode;

    public int editCounts;

    @ManyToOne
    public TopEditsExtract topExtracts;

    public static Finder<Long,TopEditsNation> find = new Finder<Long,TopEditsNation>(
            Long.class, TopEditsNation.class
    );

    public TopEditsNation(String isoCode, int editCounts) {
        this.isoCode = isoCode;
        this.editCounts = editCounts;
    }
}