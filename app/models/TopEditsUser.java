package models;

import java.util.*;

import javax.persistence.*;

import com.fasterxml.jackson.annotation.JsonIgnore;

import play.db.ebean.*;
import play.data.format.*;
import play.data.validation.*;

@Entity
public class TopEditsUser extends Model {

	private static final long serialVersionUID = -1211718927369470613L;

	@Id
	public Long id;
	
	@Constraints.Required
	public String name;

	public int editCounts;

	@ManyToOne(fetch=FetchType.EAGER)
	@JsonIgnore
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