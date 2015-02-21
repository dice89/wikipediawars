package models;

import java.util.*;
import javax.persistence.*;

import play.db.ebean.*;
import play.data.format.*;
import play.data.validation.*;

@Entity
public class TopEditsArticle extends Model {

    @Id
    public Long id;

    public int editCounts;

    @Constraints.Required
    public String label;

    @ManyToMany(mappedBy = "topArticles")
    public LinkedList<TopEditsExtract> topExtracts;

    public static Finder<Long,TopEditsArticle> find = new Finder<Long,TopEditsArticle>(
            Long.class, TopEditsArticle.class
    );

}