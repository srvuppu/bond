
#pragma once

#include <bond/core/bond_version.h>

#if BOND_VERSION < 0x302
#error This file was generated by a newer version of Bond compiler
#error and is incompatible with your version Bond library.
#endif

#if BOND_MIN_CODEGEN_VERSION > 0x0401
#error This file was generated by an older version of Bond compiler
#error and is incompatible with your version Bond library.
#endif

#include <bond/core/config.h>
#include <bond/core/containers.h>
#include <bond/core/nullable.h>
#include <bond/core/bonded.h>
#include <bond/core/blob.h>


namespace tests
{
    
    struct Foo
    {
        
        Foo()
        {
        }

        
#ifndef BOND_NO_CXX11_DEFAULTED_FUNCTIONS
        // Compiler generated copy ctor OK
        Foo(const Foo& other) = default;
#endif
        
#ifndef BOND_NO_CXX11_RVALUE_REFERENCES
        Foo(Foo&&)
        {
        }
#endif
        
        explicit
        Foo(const arena&)
        {
        }
        
        
#ifndef BOND_NO_CXX11_DEFAULTED_FUNCTIONS
        // Compiler generated operator= OK
        Foo& operator=(const Foo& other) = default;
#endif

        bool operator==(const Foo&) const
        {
            return true;
        }

        bool operator!=(const Foo& other) const
        {
            return !(*this == other);
        }

        void swap(Foo&)
        {
            using std::swap;
        }

        struct Schema;

    protected:
        void InitMetadata(const char*, const char*)
        {
        }
    };

    inline void swap(Foo& left, Foo& right)
    {
        left.swap(right);
    }

    struct Bar;

    
    struct ComplexTypes
    {
        std::list<int8_t, typename arena::rebind<int8_t>::other> li8;
        std::set<bool, std::less<bool>, typename arena::rebind<bool>::other> sb;
        std::vector<bond::blob, typename arena::rebind<bond::blob>::other> vb;
        bond::nullable<float> nf;
        std::map<std::basic_string<char, std::char_traits<char>, typename arena::rebind<char>::other>, std::basic_string<wchar_t, std::char_traits<wchar_t>, typename arena::rebind<wchar_t>::other>, std::less<std::basic_string<char, std::char_traits<char>, typename arena::rebind<char>::other> >, typename arena::rebind<std::pair<const std::basic_string<char, std::char_traits<char>, typename arena::rebind<char>::other>, std::basic_string<wchar_t, std::char_traits<wchar_t>, typename arena::rebind<wchar_t>::other> > >::other> msws;
        bond::bonded< ::tests::Foo> bfoo;
        std::map<double, std::list<std::vector<bond::nullable<bond::bonded< ::tests::Bar> >, typename arena::rebind<bond::nullable<bond::bonded< ::tests::Bar> > >::other>, typename arena::rebind<std::vector<bond::nullable<bond::bonded< ::tests::Bar> >, typename arena::rebind<bond::nullable<bond::bonded< ::tests::Bar> > >::other> >::other>, std::less<double>, typename arena::rebind<std::pair<const double, std::list<std::vector<bond::nullable<bond::bonded< ::tests::Bar> >, typename arena::rebind<bond::nullable<bond::bonded< ::tests::Bar> > >::other>, typename arena::rebind<std::vector<bond::nullable<bond::bonded< ::tests::Bar> >, typename arena::rebind<bond::nullable<bond::bonded< ::tests::Bar> > >::other> >::other> > >::other> m;
        
        ComplexTypes()
        {
        }

        
#ifndef BOND_NO_CXX11_DEFAULTED_FUNCTIONS
        // Compiler generated copy ctor OK
        ComplexTypes(const ComplexTypes& other) = default;
#endif
        
#ifndef BOND_NO_CXX11_RVALUE_REFERENCES
        ComplexTypes(ComplexTypes&& other)
          : li8(std::move(other.li8)),
            sb(std::move(other.sb)),
            vb(std::move(other.vb)),
            nf(std::move(other.nf)),
            msws(std::move(other.msws)),
            bfoo(std::move(other.bfoo)),
            m(std::move(other.m))
        {
        }
#endif
        
        explicit
        ComplexTypes(const arena& allocator)
          : li8(allocator),
            sb(std::less<bool>(), allocator),
            vb(allocator),
            nf(),
            msws(std::less<std::basic_string<char, std::char_traits<char>, typename arena::rebind<char>::other>>(), allocator),
            m(std::less<double>(), allocator)
        {
        }
        
        
#ifndef BOND_NO_CXX11_DEFAULTED_FUNCTIONS
        // Compiler generated operator= OK
        ComplexTypes& operator=(const ComplexTypes& other) = default;
#endif

        bool operator==(const ComplexTypes& other) const
        {
            return true
                && (li8 == other.li8)
                && (sb == other.sb)
                && (vb == other.vb)
                && (nf == other.nf)
                && (msws == other.msws)
                && (bfoo == other.bfoo)
                && (m == other.m);
        }

        bool operator!=(const ComplexTypes& other) const
        {
            return !(*this == other);
        }

        void swap(ComplexTypes& other)
        {
            using std::swap;
            swap(li8, other.li8);
            swap(sb, other.sb);
            swap(vb, other.vb);
            swap(nf, other.nf);
            swap(msws, other.msws);
            swap(bfoo, other.bfoo);
            swap(m, other.m);
        }

        struct Schema;

    protected:
        void InitMetadata(const char*, const char*)
        {
        }
    };

    inline void swap(ComplexTypes& left, ComplexTypes& right)
    {
        left.swap(right);
    }
} // namespace tests

#if !defined(BOND_NO_CXX11_ALLOCATOR)
namespace std
{
    template <typename _Alloc>
    struct uses_allocator< ::tests::Foo, _Alloc>
        : is_convertible<_Alloc, arena>
    {};

    template <typename _Alloc>
    struct uses_allocator< ::tests::ComplexTypes, _Alloc>
        : is_convertible<_Alloc, arena>
    {};
}
#endif
