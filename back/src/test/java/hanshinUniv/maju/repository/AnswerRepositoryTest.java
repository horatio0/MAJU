//package hanshinUniv.maju.repository;
//
//import hanshinUniv.maju.entity.*;
//import org.junit.jupiter.api.Test;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.boot.test.context.SpringBootTest;
//import org.springframework.transaction.annotation.Transactional;
//import org.springframework.data.domain.Page;
//import org.springframework.data.domain.PageRequest;
//
//import java.time.LocalDateTime;
//import java.util.List;
//import java.util.Optional;
//
//import static org.assertj.core.api.Assertions.assertThat;
//
//@SpringBootTest(properties = {
//        "spring.datasource.url=jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1",
//        "spring.datasource.driver-class-name=org.h2.Driver",
//        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
//        "spring.jpa.hibernate.ddl-auto=create-drop"
//})
//@Transactional
//public class AnswerRepositoryTest {
//
//    @Autowired
//    private AnswerRepository answerRepository;
//
//    @Autowired
//    private SessionQuestionRepository sessionQuestionRepository;
//
//    @Autowired
//    private SessionRepository sessionRepository;
//
//    @Autowired
//    private QuestionRepository questionRepository;
//
//    @Autowired
//    private UserRepository userRepository;
//
//    private User createUser() {
//        User u = User.builder()
//                .name("test")
//                .email("test@example.com")
//                .password("pwd")
//                .role("USER")
//                .regDate(LocalDateTime.now())
//                .updDate(LocalDateTime.now())
//                .build();
//        return userRepository.save(u);
//    }
//
//    private Session createSession(User user) {
//        Session s = Session.builder()
//                .sessionTitle("s1")
//                .createDate(LocalDateTime.now())
//                .user(user)
//                .build();
//        return sessionRepository.save(s);
//    }
//
//    private Question createQuestion(String text) {
//        Question q = Question.builder()
//                .category("cat")
//                .question(text)
//                .build();
//        return questionRepository.save(q);
//    }
//
//    @Test
//    public void testFindBySessionQuestionSessionId_and_pageable_and_findBySessionQuestionSessionIdAndSessionQuestionQuestionId() {
//        User user = createUser();
//        Session session = createSession(user);
//
//        Question q1 = createQuestion("q1");
//        Question q2 = createQuestion("q2");
//
//        SessionQuestion sq1 = SessionQuestion.builder().session(session).question(q1).build();
//        SessionQuestion sq2 = SessionQuestion.builder().session(session).question(q2).build();
//        sq1 = sessionQuestionRepository.save(sq1);
//        sq2 = sessionQuestionRepository.save(sq2);
//
//        Answer a1 = Answer.builder().videoUrl("/tmp/v1.mp4").sessionQuestion(sq1).build();
//        Answer a2 = Answer.builder().videoUrl("/tmp/v2.mp4").sessionQuestion(sq2).build();
//        answerRepository.save(a1);
//        answerRepository.save(a2);
//
//        List<Answer> answers = answerRepository.findBySessionQuestionSessionId(session.getId());
//        assertThat(answers).hasSize(2);
//
//        Page<Answer> page = answerRepository.findBySessionQuestionSessionId(session.getId(), PageRequest.of(0, 10));
//        assertThat(page.getTotalElements()).isEqualTo(2);
//
//        Optional<Answer> opt = answerRepository.findBySessionQuestionSessionIdAndSessionQuestionQuestionId(session.getId(), q1.getId());
//        assertThat(opt).isPresent();
//        assertThat(opt.get().getSessionQuestion().getQuestion().getId()).isEqualTo(q1.getId());
//    }
//}
