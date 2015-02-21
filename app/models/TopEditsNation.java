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

    @ManyToMany(mappedBy = "topNations")
    public LinkedList<TopEditsExtract> topExtracts;

    public static Finder<Long,TopEditsNation> find = new Finder<Long,TopEditsNation>(
            Long.class, TopEditsNation.class
    );

}