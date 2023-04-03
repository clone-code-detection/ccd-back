CREATE
OR REPLACE FUNCTION get_authorities(uuid)
    returns TABLE
            (
                authority varchar(255)
            )
AS
$$
select distinct (concat('ROLE_', role.name))
from authen."user"
         join authen.relation_user_role rur on "user".id = rur.user_id
         join authen.relation_role_authority rra on rur.role_id = rra.role_id
         join authen.role on rra.role_id = role.id
         join authen.authority on rra.authority_id = authority.id
where authen."user".id = $1

UNION ALL

select distinct authority.name as authorities
from authen."user"
         join authen.relation_user_role rur on "user".id = rur.user_id
         join authen.relation_role_authority rra on rur.role_id = rra.role_id
         join authen.role on rra.role_id = role.id
         join authen.authority on rra.authority_id = authority.id
where authen."user".id = $1;
$$
LANGUAGE SQL
    RETURNS NULL ON NULL INPUT;