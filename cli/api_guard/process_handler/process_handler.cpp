// Copyright 2026 Ahmed Yarub Hani Al Nuaimi
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//     http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

#include "process_handler.h"

#include <iostream>
#include <boost/asio/io_context.hpp>
#include <boost/process/v2/environment.hpp>

namespace bp = boost::process;
namespace asio = boost::asio;

std::string runProcess(const boost::process::v2::filesystem::path& command, const std::vector<std::string>& args, const std::vector<std::string>& envVariables)
{
    std::string output;

    try
    {
        asio::io_context ctx;
        const auto c = boost::process::environment::current();
        std::vector<boost::process::environment::key_value_pair> my_env{c.begin(), c.end()};
        my_env.insert(my_env.end(), envVariables.begin(), envVariables.end());

        bp::popen proc(ctx.get_executor(), command, args, boost::process::process_environment(my_env));

        constexpr std::size_t chunk_size = 8192;
        char buffer[chunk_size];
        boost::system::error_code ec;

        while (proc.running())
        {
            auto bytes_read = proc.read_some(asio::buffer(buffer, chunk_size), ec);
            if (ec)
                break;
            output.append(buffer, bytes_read);
        }

        while (auto bytes_read = proc.read_some(asio::buffer(buffer, chunk_size), ec))
        {
            if (ec)
                break;
            output.append(buffer, bytes_read);
        }

        proc.wait();
    }
    catch (const std::exception& e)
    {
        std::cerr << "[Process Error] " << e.what() << '\n';
    }
    catch (...)
    {
        std::cerr << "[Unknown Error] Something went wrong.\n";
    }

    return output;
}

boost::process::v2::filesystem::path getExecutablePath(const std::string& exe)
{
    return bp::environment::find_executable(exe);
}