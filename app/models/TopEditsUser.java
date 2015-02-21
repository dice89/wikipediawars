package models;

import java.util.*;

import javax.persistence.*;

import play.db.ebean.*;
import play.data.format.*;
import play.data.validation.*;

@Entity
public class TopEditsUser extends Model {

	@Id
	public Long id;
	
	@Constraints.Required
	public String name;

	public int editCounts;

    @ManyToOne(fetch=FetchType.LAZY)
    public TopEditsExtract topExtracts;


	public static Finder<Long, TopEditsUser> find = new Finder<Long, TopEditsUser>(
			Long.class, TopEditsUser.class);

	public TopEditsUser(String name, int editCounts) {
		super();
		this.name = name;
		this.editCounts = editCounts;
	}

	public TopEditsUser() {
		super();
	}
	
	
}