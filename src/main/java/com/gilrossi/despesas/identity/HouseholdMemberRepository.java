package com.gilrossi.despesas.identity;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface HouseholdMemberRepository extends JpaRepository<HouseholdMember, Long> {

	@Query("""
		select member
		from HouseholdMember member
		join fetch member.household household
		join fetch member.user user
		where household.id = :householdId
		  and household.deletedAt is null
		  and user.deletedAt is null
		  and member.deletedAt is null
		order by case when member.role = com.gilrossi.despesas.identity.HouseholdMemberRole.OWNER then 0 else 1 end, member.id asc
		""")
	List<HouseholdMember> findActiveMembershipsByHouseholdId(@Param("householdId") Long householdId);

	@Query("""
		select member
		from HouseholdMember member
		join fetch member.household household
		join fetch member.user user
		where user.id = :userId
		  and user.deletedAt is null
		  and household.deletedAt is null
		  and member.deletedAt is null
		order by member.id asc
		""")
	List<HouseholdMember> findActiveMembershipsByUserId(@Param("userId") Long userId);

	default Optional<HouseholdMember> findFirstActiveMembershipByUserId(Long userId) {
		return findActiveMembershipsByUserId(userId).stream().findFirst();
	}
}
