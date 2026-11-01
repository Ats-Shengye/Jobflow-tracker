package com.jobflow.tracker.repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import com.jobflow.tracker.entity.JobApplication;

// 他人の応募を読めないよう、JpaRepository は継承せず findById / findAll を出さない。取得は持ち主で絞るものだけ。
public interface JobApplicationRepository extends Repository<JobApplication, Long> {

	// save と delete は CrudRepository と同じシグネチャにして、Spring Data の既定の実装を使う。
	<S extends JobApplication> S save(S application);

	void delete(JobApplication application);

	// 申込日が同じ時は ID の降順にして、並びを毎回同じにする。
	List<JobApplication> findByOwnerIdOrderByAppliedDateDescIdDesc(Long ownerId);

	// 他人の応募 ID なら空を返す（API では 404 にする）。
	Optional<JobApplication> findByIdAndOwnerId(Long id, Long ownerId);

	long countByOwnerId(Long ownerId);

	// 初期データ専用。一括 update は @PreUpdate を通らないので、デモの応募の最終更新日時を過去の日時にできる。
	// 他の取得と同じく、持ち主で絞った行だけを書き換える。
	@Modifying(flushAutomatically = true, clearAutomatically = true)
	@Query("update JobApplication a set a.updatedAt = :updatedAt where a.id = :id and a.owner.id = :ownerId")
	int overwriteUpdatedAtForSeedData(@Param("id") Long id, @Param("ownerId") Long ownerId,
			@Param("updatedAt") Instant updatedAt);
}
