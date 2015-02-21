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

    @OneToMany(mappedBy = "topExtracts",cascade = CascadeType.ALL)
    public List<TopEditsUser> topUser= new LinkedList<>();

    @OneToMany(mappedBy = "topExtracts",cascade = CascadeType.ALL)
    public List<TopEditsArticle> topArticles = new LinkedList<>();

    @OneToMany(mappedBy = "topExtracts",cascade = CascadeType.ALL)
    public List<TopEditsNation> topNations= new LinkedList<>();

    @Constraints.Required
    @Formats.DateTime(pattern="dd/MM/yyyy")
    public Date timestamp;

    public static Finder<Long,TopEditsExtract> find = new Finder<Long,TopEditsExtract>(
            Long.class, TopEditsExtract.class
    );

    public TopEditsExtract(Date timestamp) {
    	super();
        this.timestamp = timestamp;
    }

	public TopEditsExtract() {
		super();
	}
    

}