
#Users per country:
#===================
Select 
count(*) as no_of_user, 
country 
from wikiwars.users 
where country is not null 
group by country 
order by no_of_user desc;

Select count(*) as no_of_user, country from wikiwars.users where country is not null group by country order by no_of_user desc limit 20;


#User per non english country:

Select 
count(*) as no_of_user,
country
from wikiwars.users
where country is not null and country !=  'US' and country != 'CA' and country != 'IN' and country != 'GB' and country != 'IE' and country != 'AU' group by country order by no_of_user desc;


#Edits per non english country:

Select sum(editcount) as edits, country from wikiwars.users where country is not null and country !=  'US' and country != 'CA' and country != 'IN' and country != 'GB' and country != 'IE' and country != 'AU' and country != 'NZ'  group by country order by edits desc limit 20;

#Edit per english speaking country:

Select sum(editcount) as edits, country from wikiwars.users where country is not null group by country order by edits desc limit 20;


# Getting user with the highest edit score

Select name, country  from wikiwars.users where editcount = (Select max(editcount) from wikiwars.users where country is not null);

#Users and Edits per country:
#=============================

Select count(*) as no_of_user, 
sum(editcount) as no_of_edits,country 
from wikiwars.users 
where country is not null 
group by country 
order by no_of_user desc;


#Edits per country ordered by edits per capita:
#===============================================
Select 
count(*)as no_of_user, 
sum(editcount) as no_of_edits, 
country, 
max(p.population) as population, 
sum(editcount)/max(population) as edits_per_capita
from wikiwars.users u
full outer join wikiwars.population2 p on u.country = p.code2
where u.country is not null and p.population is not null
group by u.country
order by edits_per_capita desc limit 20;


#Power Law distribution country users: log log scale:
#======================================================

select 
round(log(editcount)::numeric,3) as bucket, 
log(count(*))+1 as count 
from wikiwars.users 
where country is not null 
group by bucket 
order by bucket;


#Power Law distribution users: log log scale:
#============================================

select 
round(log(editcount)::numeric,3) as bucket, 
log(count(*))+1 as count 
from wikiwars.users 
group by bucket 
order by bucket;