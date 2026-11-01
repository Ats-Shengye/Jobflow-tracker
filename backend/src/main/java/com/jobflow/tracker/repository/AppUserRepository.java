package com.jobflow.tracker.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.jobflow.tracker.entity.AppUser;
import com.jobflow.tracker.entity.Role;

public interface AppUserRepository extends JpaRepository<AppUser, Long> {

	Optional<AppUser> findByLoginId(String loginId);

	// 同時に登録されてこの確認をすり抜けた場合は、UNIQUE 制約（uk_users_login_id）で止まる。
	boolean existsByLoginId(String loginId);

	// 求職者ごとに応募を読み直すと N+1 になるので、1本のクエリで集計する。left join で応募0件の人も残す。
	@Query("""
			select new com.jobflow.tracker.repository.SeekerSummary(u.id, u.name, count(a.id), max(a.updatedAt))
			from AppUser u
			left join JobApplication a on a.owner = u
			where u.role = :role
			group by u.id, u.name
			order by u.id""")
	List<SeekerSummary> findSummariesByRole(@Param("role") Role role);

	default List<SeekerSummary> findSeekerSummaries() {
		return findSummariesByRole(Role.SEEKER);
	}
}
