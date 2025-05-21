package com.example.audiotext.service.impl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.persistence.criteria.Expression;
import javax.persistence.criteria.Predicate;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import com.example.audiotext.dto.UserCreateTranrq;
import com.example.audiotext.dto.UserDeleteTranrq;
import com.example.audiotext.dto.UserLoginTranrq;
import com.example.audiotext.dto.UserLoginTranrs;
import com.example.audiotext.dto.UserQueryTranrq;
import com.example.audiotext.dto.UserQueryTranrs;
import com.example.audiotext.dto.UserUpdateTranrq;
import com.example.audiotext.entity.UserEntity;
import com.example.audiotext.repository.RoleRepository;
import com.example.audiotext.repository.UserRepository;
import com.example.audiotext.service.UserService;

@Service
public class UserServiceImpl implements UserService {

	@Autowired
	private UserRepository userRepository;

	
	@Autowired
	private RoleRepository roleRepository;

	@Override
	public UserLoginTranrs login(UserLoginTranrq rq) {
		UserLoginTranrs rs = new UserLoginTranrs();

		return userRepository.findByUsername(rq.getUsername())
				.filter(user -> user.getPassword().equals(rq.getPassword())).map(user -> {
					rs.setMessage("登入成功");
					rs.setUsername(user.getUsername());

					// 查 role 表
					roleRepository.findByRoles(user.getRoles())
							.ifPresent(roleEntity -> rs.setRolename(roleEntity.getRolename()));

					return rs;
				}).orElseGet(() -> {
					rs.setMessage("登入失敗");
					return rs;
				});
	}

	@Override
	public ResponseEntity<Map<String, String>> createUser(UserCreateTranrq rq) {
		UserEntity user = new UserEntity();
		user.setUsername(rq.getUsername());
		user.setPassword(rq.getPassword());
		user.setRoles(rq.getRoles());

		userRepository.save(user);

		Map<String, String> response = new HashMap<>();
		response.put("message", "新增成功");
		return ResponseEntity.ok(response);
	}

	@Override
	public ResponseEntity<Map<String, String>> deleteUser(UserDeleteTranrq rq) {
		Optional<UserEntity> optionalUser = userRepository.findById(rq.getId());
		Map<String, String> response = new HashMap<>();

		if (optionalUser.isPresent()) {
			userRepository.deleteById(rq.getId());
			response.put("message", "刪除成功");
			return ResponseEntity.ok(response);
		} else {
			response.put("message", "刪除失敗，找不到資料");
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
		}
	}

	@Override
	public Map<String, String> updateUser(UserUpdateTranrq rq) {
		Map<String, String> map = new HashMap<>();
		Optional<UserEntity> UserOptional = userRepository.findById(rq.getId());
		if (UserOptional.isPresent()) {
			UserEntity user = UserOptional.get();
			user.setUsername(rq.getUsername());
			if (rq.getPassword() != null && !rq.getPassword().isEmpty()) {
				user.setPassword(rq.getPassword());
			}
			user.setRoles(rq.getRoles());
			userRepository.save(user);
			map.put("returnDesc", "更新成功");
		} else {
			map.put("returnDesc", "更新失敗");

		}
		return map;
	}

	@Override
	public UserQueryTranrs queryUser(UserQueryTranrq request) {
		int pageNumber = request.getPageNumber() != null ? request.getPageNumber() - 1 : 0;
		int pageSize = request.getPageSize() != null ? request.getPageSize() : 50;

		String sortField = StringUtils.hasText(request.getSidx()) ? request.getSidx() : "roles";
		Sort.Direction sortDir = "asc".equalsIgnoreCase(request.getSord()) ? Sort.Direction.ASC : Sort.Direction.DESC;

		Sort sort;
		if ("updatetime".equalsIgnoreCase(sortField)) {
			sort = Sort.by(sortDir, "roles").and(Sort.by(Sort.Direction.ASC, "id"));
		} else {
			sort = Sort.by(sortDir, sortField);
		}

		Pageable pageable = PageRequest.of(pageNumber, pageSize, sort);

		Specification<UserEntity> spec = (root, query, cb) -> {
			List<Predicate> predicates = new ArrayList<>();

			if (request.getId() != null) {
				// 將 id 欄位轉成字串做 like 查詢
				Expression<String> idAsString = cb.concat(root.get("id").as(String.class), "");
				predicates.add(cb.like(idAsString, "%" + request.getId() + "%"));
			}

			return cb.and(predicates.toArray(new Predicate[0]));
		};
		Page<UserEntity> page = userRepository.findAll(spec,pageable);
		return new UserQueryTranrs(page.getContent(), page.getTotalPages(), page.getTotalElements(),
				page.getNumber() + 1, page.getSize());

	}
}