package com.remaslover.libraryapp.controller;


import com.remaslover.libraryapp.dto.BookDto;
import com.remaslover.libraryapp.entity.Book;
import com.remaslover.libraryapp.entity.User;
import com.remaslover.libraryapp.mapper.BookMapper;
import com.remaslover.libraryapp.mapper.UserMapper;
import com.remaslover.libraryapp.service.BookService;
import com.remaslover.libraryapp.service.UserService;
import com.remaslover.libraryapp.util.BookValidator;
import com.remaslover.libraryapp.util.UserValidator;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Controller
@RequestMapping("/books")
public class BookController {

    private final BookService bookService;
    private final UserService userService;
    private final UserMapper userMapper;
    private final BookMapper bookMapper;
    private final UserValidator userValidator;
    private final BookValidator bookValidator;

    public BookController(BookService bookService, UserService userService, UserMapper userMapper, BookMapper bookMapper, UserValidator userValidator, BookValidator bookValidator) {
        this.bookService = bookService;
        this.userService = userService;
        this.userMapper = userMapper;
        this.bookMapper = bookMapper;
        this.userValidator = userValidator;
        this.bookValidator = bookValidator;
    }

    @GetMapping("/search")
    public String search(Model model, @RequestParam(required = false) String query) {
        if (query != null && !query.isEmpty()) {
            List<Book> books = bookService.getByTitleStartingWithIgnoreCase(query);
            if (books.isEmpty()) {
                model.addAttribute("message", "По вашему запросу ничего не найдено.");
            } else {
                model.addAttribute("books", books);
            }
        } else {
            model.addAttribute("message", "Пожалуйста, введите запрос для поиска.");
        }
        return "book/search_book";
    }



    @GetMapping
    public String getBooks(
            Model model,
            @RequestParam(required = false, name = "sort_by_year") String sortByYear,
            @RequestParam(required = false, defaultValue = "0", name = "page") int page,
            @RequestParam(required = false, defaultValue = "3", name = "size") int size
    ) {
        Pageable pageable = PageRequest.of(page, size);

        if ("true".equalsIgnoreCase(sortByYear)) {
            pageable = PageRequest.of(page, size, Sort.by(Sort.Order.asc("year")));
        }

        Page<Book> booksPage = bookService.getBooksByPage(pageable);

        model.addAttribute("books", bookMapper.mapToDto(booksPage.getContent()));
        model.addAttribute("totalPages", booksPage.getTotalPages());
        model.addAttribute("currentPage", booksPage.getNumber());
        model.addAttribute("totalBooks", booksPage.getTotalElements());
        model.addAttribute("pageSize", size);

        return "book/books";
    }

    @GetMapping("/{id}")
    public String getBook(@PathVariable long id, Model model) {
        model.addAttribute("book", bookMapper.mapToDto(bookService.getById(id)));
        model.addAttribute("user", bookMapper.mapToDto(bookService.getUserByBookId(id).get()));
        model.addAttribute("users", userMapper.mapToDto(userService.getUsers()));
        return "book/book_info";
    }

    @PostMapping("/new")
    public String newBook(@ModelAttribute @Validated BookDto bookDto, BindingResult bindingResult, Model model) {

        bookValidator.validate(bookDto, bindingResult);

        if (bindingResult.hasErrors()) {
            model.addAttribute("bookDto", bookDto);
            return "book/add_book"; // Вернуться к форме с ошибками
        }
        bookService.save(bookMapper.mapToEntity(bookDto));
        return "redirect:/books";
    }

    @GetMapping("/new")
    public String showAddBookForm(@ModelAttribute BookDto bookDto) {
        return "book/add_book";
    }

    @PostMapping("/{id}/assign")
    public String assignUserToBook(@PathVariable Long id, @ModelAttribute BookDto book) {
        Book existingBook = bookService.getById(id);
        System.out.println(existingBook.getId());
        User selectedUser = userService.getUserById(book.getUser().getId());
        System.out.println(selectedUser.getId());
        existingBook.setUser(selectedUser);
        bookService.save(existingBook);
        return "redirect:/books";
    }

    @PostMapping("/edit/{id}")
    public String editBook(@ModelAttribute @Validated BookDto bookDto, @PathVariable Long id, Model model, BindingResult bindingResult) {
        bookValidator.validate(bookDto, bindingResult);


        if (bindingResult.hasErrors()) {
            model.addAttribute("bookDto", bookDto);
            return "book/edit_book";
        }

        bookDto.setId(id);
        bookService.update(bookMapper.mapToEntity(bookDto));

        return "redirect:/books";
    }

    @GetMapping("/edit/{id}")
    public String showEditBookForm(@PathVariable long id, Model model) {
        model.addAttribute("bookDto", bookMapper.mapToDto(bookService.getById(id)));
        return "book/edit_book";
    }

    @PostMapping("/{id}")
    public String deleteBook(@PathVariable long id) {
        bookService.delete(id);
        return "redirect:/books";
    }

    @PostMapping("/{id}/release")
    public String releaseBook(@PathVariable Long id) {
        Book book = bookService.getById(id);
        book.setUser(null);
        bookService.save(book);
        return "redirect:/books";
    }


}
