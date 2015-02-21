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

    @ManyToMany
    public LinkedList<TopEditsUser> topUser;

    @ManyToMany
    public LinkedList<TopEditsArticle> topArticles;

    @ManyToMany
    public LinkedList<TopEditsNation> topNations;

    @Constraints.Required
    @Formats.DateTime(pattern="dd/MM/yyyy")
    public Date timestamp;

    public static Finder<Long,TopEditsExtract> find = new Finder<Long,TopEditsExtract>(
            Long.class, TopEditsExtract.class
    );

}