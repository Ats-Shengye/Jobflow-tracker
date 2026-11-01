package com.jobflow.tracker.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.repository.Repository;

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
}
