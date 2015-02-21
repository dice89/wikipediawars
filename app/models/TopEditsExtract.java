package models;

import java.util.*;
import javax.persistence.*;

import play.db.ebean.*;
import play.data.format.*;
import play.data.validation.*;

@Entity
public class TopEditsExtract extends Model {

    @Id
    public Long id;

    @ManyToMany(cascade = CascadeType.ALL)
    public LinkedList<TopEditsUser> topUser= new LinkedList<>();

    @ManyToMany(cascade = CascadeType.ALL)
    public LinkedList<TopEditsArticle> topArticles = new LinkedList<>();

    @ManyToMany(cascade = CascadeType.ALL)
    public LinkedList<TopEditsNation> topNations= new LinkedList<>();

    @Constraints.Required
    @Formats.DateTime(pattern="dd/MM/yyyy")
    public Date timestamp;

    public static Finder<Long,TopEditsExtract> find = new Finder<Long,TopEditsExtract>(
            Long.class, TopEditsExtract.class
    );

    public TopEditsExtract(Date timestamp) {
        this.timestamp = timestamp;
    }

}