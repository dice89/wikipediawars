package models;

import java.util.*;

import javax.persistence.*;

import com.fasterxml.jackson.annotation.JsonIgnore;

import play.db.ebean.*;
import play.data.format.*;
import play.data.validation.*;

@Entity
public class TopEditsNation extends Model {

	private static final long serialVersionUID = 7893549669380111007L;

	@Id
	public Long id;

	@Constraints.Required
	public String isoCode;

	public int editCounts;

    @ManyToOne(fetch=FetchType.EAGER)
    @JsonIgnore
    public TopEditsExtract topExtracts;


	public static Finder<Long, TopEditsNation> find = new Finder<Long, TopEditsNation>(
			Long.class, TopEditsNation.class);

	public TopEditsNation(String isoCode, int editCounts) {
		super();
		this.isoCode = isoCode;
		this.editCounts = editCounts;
	}
}